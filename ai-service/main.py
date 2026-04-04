import os
import json
import re
from fastapi import FastAPI, UploadFile, File, HTTPException
import uvicorn
from io import BytesIO
from PIL import Image

import easyocr
import fitz  # PyMuPDF
from transformers import AutoModelForCausalLM, AutoTokenizer
import torch

app = FastAPI(title="Archive Voice AI Service")

# Paths to local models
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
OCR_MODEL_DIR = os.path.join(BASE_DIR, "models", "easy_ocr", "root", ".EasyOCR", "model")
LLM_MODEL_DIR = os.path.join(BASE_DIR, "models", "qwen_llm", "backend_llm_model")

DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
print(f"[INFO] Using device: {DEVICE}")

print("[INFO] Loading EasyOCR models...")
reader = easyocr.Reader(['ru'], gpu=(DEVICE == 'cuda'), model_storage_directory=OCR_MODEL_DIR, download_enabled=False)

print("[INFO] Loading Qwen LLM...")
tokenizer = AutoTokenizer.from_pretrained(LLM_MODEL_DIR)

DTYPE = torch.float16 if DEVICE == 'cuda' else torch.float32

model = AutoModelForCausalLM.from_pretrained(
    LLM_MODEL_DIR,
    device_map=DEVICE,
    torch_dtype=DTYPE
)
model.eval()

PROMPT_TEMPLATE = """Извлеки информацию о репрессированном человеке из текста документа и верни СТРОГО один валидный JSON объект без markdown форматирования.
Поля JSON:
- fullName (ФИО)
- birthYear (год рождения, например "1901")
- deathYear (год смерти или null)
- region (СТРОГО ОДНО ИЗ: "regions.chuy", "regions.osh", "regions.issyk_kul", "regions.naryn", "regions.jalal_abad", "regions.talas", "regions.batken" или пустая строка)
- district (район)
- occupation (профессия)
- accusation (СТРОГО ОДНО ИЗ: "accusations.agitation", "accusations.organization", "accusations.espionage", "accusations.sabotage", "accusations.terror", "accusations.treason", "accusations.other" или пустая строка)
- arrestDate (дата ареста, например "1937-11-10")
- sentence (приговор, например "10 лет ИТЛ")
- sentenceDate (дата приговора)
- rehabilitationDate (дата реабилитации)
- biography (краткая выдержка текста, биография)
- source (номер дела или источник)

Текст документа:
{text}

Верни ТОЛЬКО JSON объект:"""

def parse_pdf(file_bytes: bytes) -> str:
    text_content = []
    doc = fitz.open(stream=file_bytes, filetype="pdf")
    # For now, just process the first 2 pages to save time on CPU
    for page_num in range(min(2, len(doc))):
        page = doc[page_num]
        pix = page.get_pixmap()
        img = Image.frombytes("RGB", [pix.width, pix.height], pix.samples)
        
        # Convert image to bytes for easyocr
        img_byte_arr = BytesIO()
        img.save(img_byte_arr, format='PNG')
        img_bytes = img_byte_arr.getvalue()
        
        ocr_result = reader.readtext(img_bytes, detail=0)
        text_content.append(" ".join(ocr_result))
    return " ".join(text_content)

def parse_image(file_bytes: bytes) -> str:
    ocr_result = reader.readtext(file_bytes, detail=0)
    return " ".join(ocr_result)

def extract_json(text: str) -> dict:
    # Try to find JSON block in the output
    match = re.search(r'\{.*\}', text, re.DOTALL)
    if match:
        try:
            return json.loads(match.group(0))
        except json.JSONDecodeError:
            pass
    return {}

@app.post("/parse")
async def parse_document(file: UploadFile = File(...)):
    try:
        content = await file.read()
        
        filename = file.filename.lower()
        print(f"[INFO] Processing file: {filename}")
        
        extracted_text = ""
        if filename.endswith(".pdf"):
            extracted_text = parse_pdf(content)
        elif filename.endswith((".png", ".jpg", ".jpeg")):
            extracted_text = parse_image(content)
        else:
            raise HTTPException(status_code=400, detail="Unsupported file format. Use PDF, PNG, or JPG.")
            
        if not extracted_text.strip():
            return {"error": "No text detected in document."}
            
        print(f"[INFO] Extracted text length: {len(extracted_text)}")
        
        # Limit text length to avoid context overflow on CPU (and speed up)
        prompt_text = PROMPT_TEMPLATE.format(text=extracted_text[:1500])
        
        messages = [
            {"role": "system", "content": "You are a helpful assistant that strictly outputs JSON data based on historical documents."},
            {"role": "user", "content": prompt_text}
        ]
        
        text_input = tokenizer.apply_chat_template(
            messages, tokenize=False, add_generation_prompt=True
        )
        model_inputs = tokenizer([text_input], return_tensors="pt").to(DEVICE)
        
        print("[INFO] Generating JSON using LLM...")
        with torch.no_grad():
            generated_ids = model.generate(
                **model_inputs, 
                max_new_tokens=512,
                temperature=0.1
            )
            
        generated_ids = [
            output_ids[len(input_ids):] for input_ids, output_ids in zip(model_inputs.input_ids, generated_ids)
        ]
        
        response = tokenizer.batch_decode(generated_ids, skip_special_tokens=True)[0]
        parsed_json = extract_json(response)
        
        if not parsed_json:
            print("[WARN] LLM returned unstructured text:", response)
            parsed_json = {"error": "Failed to parse JSON", "raw_response": response}
            
        return parsed_json

    except Exception as e:
        print(f"[ERROR] {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8002, workers=1)
