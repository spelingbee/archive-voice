/**
 * features/archive/constants.ts
 * Общие справочники домена «Архив».
 *
 * Единый источник данных для фильтров, форм и валидации.
 * Используется в: ArchiveFilters, add.vue, ContextChat.
 */

/** Области Кыргызстана */
export const REGIONS = [
  'regions.chuy',
  'regions.osh',
  'regions.issyk_kul',
  'regions.naryn',
  'regions.jalal_abad',
  'regions.talas',
  'regions.batken',
] as const

/** Области с value/label — для select-фильтров */
export const REGION_OPTIONS = [
  { value: '', label: 'archive.filters.all_regions' },
  { value: 'Чүйская область', label: 'regions.chuy' },
  { value: 'Ошская область', label: 'regions.osh' },
  { value: 'Иссык-Кульская область', label: 'regions.issyk_kul' },
  { value: 'Нарынская область', label: 'regions.naryn' },
  { value: 'Таласская область', label: 'regions.talas' },
  { value: 'Джалал-Абадская область', label: 'regions.jalal_abad' },
  { value: 'Баткенская область', label: 'regions.batken' },
] as const

/** Статьи обвинения (УК РСФСР, ст. 58) */
export const ACCUSATIONS = [
  'accusations.agitation',
  'accusations.organization',
  'accusations.espionage',
  'accusations.sabotage',
  'accusations.terror',
  'accusations.treason',
  'accusations.other',
] as const

/** Статьи обвинения с value/label — для select-фильтров */
export const ACCUSATION_OPTIONS = [
  { value: '', label: 'archive.filters.all_accusations' },
  { value: '58-10', label: 'accusations.agitation' },
  { value: '58-11', label: 'accusations.organization' },
  { value: '58-6', label: 'accusations.espionage' },
  { value: '58-7', label: 'accusations.sabotage' },
  { value: '58-8', label: 'accusations.terror' },
  { value: 'other', label: 'accusations.other' },
] as const

/** Десятилетия репрессий — для фильтра */
export const DECADE_OPTIONS = [
  { value: '', label: 'archive.filters.all_years' },
  { value: '1920s', label: '1920-е' },
  { value: '1930s', label: '1930-е' },
  { value: '1940s', label: '1940-е' },
  { value: '1950s', label: '1950-е' },
] as const
