import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/useAuthStore';
import UniversitySelect from '../../features/university/ui/UniversitySelect';
import { useUniversitySearchParam } from '../../shared/university/useUniversitySearchParam';

const Header: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuthStore();
  const { universityCode, setUniversityCode } = useUniversitySearchParam();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <header className="header">
      <div className="container header__inner">
        <Link to="/" className="brand">
          <div className="brand__logo" aria-hidden="true">
            PB
          </div>
          <div>
            <span className="brand__name">POLYBOOK</span>
            <span className="brand__tagline">Учебные пространства Петербурга</span>
          </div>
        </Link>
        <nav className="nav">
          <Link to="/rooms" className="nav__link">
            Аудитории
          </Link>
          <Link to="/schedule" className="nav__link">
            Расписание
          </Link>
          {isAuthenticated && (
            <Link to="/my-bookings" className="nav__link">
              Мои бронирования
            </Link>
          )}
        </nav>
        <div className="header__actions">
          <UniversitySelect
            id="header-university"
            label="Вуз"
            value={universityCode}
            onChange={setUniversityCode}
            compact
          />
          {isAuthenticated ? (
            <>
              <span className="header__login">
                {user?.firstname && user?.lastname 
                  ? `${user.firstname} ${user.lastname}` 
                  : user?.name || user?.email}
              </span>
              <button className="header__cta" onClick={handleLogout}>
                Выйти
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="header__login">
                Войти
              </Link>
              <Link to="/register" className="header__cta">
                Регистрация
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
};

export default Header;
