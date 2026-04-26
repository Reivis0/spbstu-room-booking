import React from 'react';
import {
  UNIVERSITIES,
  UniversityCode,
  getUniversity,
} from '../../../shared/university/universities';

interface UniversitySelectProps {
  id?: string;
  label?: string;
  value: UniversityCode;
  onChange: (value: UniversityCode) => void;
  className?: string;
  compact?: boolean;
}

const UniversitySelect: React.FC<UniversitySelectProps> = ({
  id = 'university',
  label = 'Вуз',
  value,
  onChange,
  className = '',
  compact = false,
}) => {
  const selectedUniversity = getUniversity(value);

  return (
    <div className={`university-select ${compact ? 'university-select--compact' : ''} ${className}`.trim()}>
      <label className="university-select__label" htmlFor={id}>
        {label}
      </label>
      <div className="university-select__control">
        <span className="university-select__badge" aria-hidden="true">
          {selectedUniversity.shortLabel}
        </span>
        <select
          id={id}
          aria-label={label}
          value={value}
          onChange={(event) => onChange(event.target.value as UniversityCode)}
          className="university-select__native"
        >
          {UNIVERSITIES.map((university) => (
            <option key={university.code} value={university.code}>
              {university.label}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
};

export default UniversitySelect;
