import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  DEFAULT_UNIVERSITY,
  UniversityCode,
  getUniversity,
  normalizeUniversityCode,
} from './universities';

export const useUniversitySearchParam = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const universityCode = normalizeUniversityCode(searchParams.get('university'));

  const setUniversityCode = useCallback(
    (nextUniversity: UniversityCode) => {
      setSearchParams((currentParams) => {
        const nextParams = new URLSearchParams(currentParams);
        if (nextUniversity === DEFAULT_UNIVERSITY) {
          nextParams.delete('university');
        } else {
          nextParams.set('university', nextUniversity);
        }
        nextParams.delete('building');
        return nextParams;
      });
    },
    [setSearchParams]
  );

  return useMemo(
    () => ({
      universityCode,
      university: getUniversity(universityCode),
      setUniversityCode,
    }),
    [setUniversityCode, universityCode]
  );
};
