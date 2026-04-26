export type UniversityCode = 'SPBSTU' | 'SPBU' | 'LETI';

export interface University {
  code: UniversityCode;
  apiValue: string;
  backendId: string;
  label: string;
  shortLabel: string;
  badgeLabel: string;
}

export const DEFAULT_UNIVERSITY: UniversityCode = 'SPBSTU';

export const UNIVERSITIES: University[] = [
  {
    code: 'SPBSTU',
    apiValue: 'SPBSTU',
    backendId: 'spbptu',
    label: 'СПбПУ',
    shortLabel: 'ПУ',
    badgeLabel: 'СПбПУ',
  },
  {
    code: 'SPBU',
    apiValue: 'SPBU',
    backendId: 'spbgu',
    label: 'СПбГУ',
    shortLabel: 'ГУ',
    badgeLabel: 'СПбГУ',
  },
  {
    code: 'LETI',
    apiValue: 'LETI',
    backendId: 'leti',
    label: 'ЛЭТИ',
    shortLabel: 'ЛЭ',
    badgeLabel: 'ЛЭТИ',
  },
];

export const UNIVERSITY_BY_CODE = UNIVERSITIES.reduce<Record<UniversityCode, University>>(
  (acc, university) => {
    acc[university.code] = university;
    return acc;
  },
  {} as Record<UniversityCode, University>
);

export const normalizeUniversityCode = (value?: string | null): UniversityCode => {
  if (!value) {
    return DEFAULT_UNIVERSITY;
  }

  const raw = value.trim();
  const normalized = raw.toUpperCase();
  const match = UNIVERSITIES.find((university) => {
    return (
      university.code === normalized ||
      university.apiValue === normalized ||
      university.backendId === raw.toLowerCase()
    );
  });

  return match?.code || DEFAULT_UNIVERSITY;
};

export const getUniversity = (value?: string | null): University => {
  return UNIVERSITY_BY_CODE[normalizeUniversityCode(value)];
};

export const inferUniversityCode = (value?: string | null): UniversityCode | undefined => {
  if (!value) {
    return undefined;
  }

  const raw = value.toLowerCase();
  return UNIVERSITIES.find((university) => {
    return (
      raw === university.code.toLowerCase() ||
      raw === university.apiValue.toLowerCase() ||
      raw === university.backendId ||
      raw.includes(`_${university.backendId}_`) ||
      raw.endsWith(`_${university.backendId}`)
    );
  })?.code;
};
