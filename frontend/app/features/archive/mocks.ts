import type { Person, PersonId } from './types'
import type { PaginatedResponse } from '~/types'

/** 
 * Моковые данные для демонстрации архива.
 * Содержат реальные исторические паттерны репрессий в Киргизской ССР.
 */
export const MOCK_PERSONS: Person[] = [
  {
    id: 1 as PersonId,
    fullName: 'Байтемиров Асан Жумабекович',
    birthYear: 1899,
    deathYear: 1937,
    region: 'Чүйская область',
    status: 'verified',
    accusation: 'Ст. 58-10 (Антисоветская агитация), участие в контрреволюционной буржуазно-националистической организации.',
  },
  {
    id: 2 as PersonId,
    fullName: 'Исаев Касымбек',
    birthYear: 1894,
    deathYear: 1938,
    region: 'Ошская область',
    status: 'verified',
    accusation: 'Ст. 58-2, 58-7, 58-8 (Вредительство и террор). Бывший нарком просвещения Киргизской ССР.',
  },
  {
    id: 3 as PersonId,
    fullName: 'Абдрахманов Юсуп',
    birthYear: 1901,
    deathYear: 1938,
    region: 'Иссык-Кульская область',
    status: 'pending',
    accusation: 'Ст. 58-1а (Измена Родине). Первый председатель Совета Народных Комиссаров Киргизской АССР.',
  },
  {
    id: 4 as PersonId,
    fullName: 'Сыдыков Абдыкерим',
    birthYear: 1889,
    deathYear: 1938,
    region: 'Чүйская область',
    status: 'verified',
    accusation: 'Ст. 58-11 (Организационная деятельность). Видный общественный деятель, основатель киргизской государственности.',
  },
  {
    id: 5 as PersonId,
    fullName: 'Тыныстанов Касым',
    birthYear: 1901,
    deathYear: 1938,
    region: 'Иссык-Кульская область',
    status: 'verified',
    accusation: 'Ст. 58-10 (Национализм). Учёный, поэт, основоположник киргизской письменности.',
  },
  {
    id: 6 as PersonId,
    fullName: 'Орозбеков Абдукадыр',
    birthYear: 1889,
    deathYear: 1938,
    region: 'Ошская область',
    status: 'verified',
    accusation: 'Ст. 58-8 (Террор). Первый председатель ЦИК Киргизской АССР.',
  },
  {
    id: 7 as PersonId,
    fullName: 'Айдарбеков Иманалы',
    birthYear: 1884,
    deathYear: 1938,
    region: 'Чүйская область',
    status: 'pending',
    accusation: 'Ст. 58-2. Участник восстания 1916 года, советский партийный деятель.',
  },
  {
    id: 8 as PersonId,
    fullName: 'Рыскулов Турар',
    birthYear: 1894,
    deathYear: 1938,
    region: 'Чүйская область',
    status: 'verified',
    accusation: 'Пантюркизм, контрреволюционная деятельность. Государственный деятель Туркестана.',
  }
]

/** Хелпер для создания пагинированного ответа из моков */
export const getMockPaginatedResponse = (search?: string): PaginatedResponse<Person> => {
  let filtered = [...MOCK_PERSONS]
  if (search) {
    const q = search.toLowerCase()
    filtered = filtered.filter(p => 
      p.fullName.toLowerCase().includes(q) || 
      p.region.toLowerCase().includes(q) ||
      p.accusation.toLowerCase().includes(q)
    )
  }
  
  return {
    content: filtered,
    totalElements: filtered.length,
    totalPages: 1,
    size: 20,
    page: 0
  }
}
