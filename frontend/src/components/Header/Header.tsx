import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/useAuthStore';

const Header: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuthStore();
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
            <img 
              src="/assets/spbstu-logo.png" 
              alt="СПбПУ" 
              style={{
                width: '100%',
                height: '100%',
                objectFit: 'contain',
                display: 'block'
              }}
              onError={(e) => {
                const target = e.target as HTMLImageElement;
                target.style.display = 'none';
                const parent = target.parentElement;
                if (parent && !parent.textContent) {
                  parent.textContent = 'СПбПУ';
                  parent.style.display = 'flex';
                  parent.style.alignItems = 'center';
                  parent.style.justifyContent = 'center';
                }
              }}
            />
          </div>
          <div>
            <span className="brand__name">Главный корпус</span>
            <span className="brand__tagline">Бронирование аудиторий</span>
          </div>
        </Link>
        <nav className="nav">
          <Link to="/rooms" className="nav__link">
            Аудитории
          </Link>
          {isAuthenticated && (
            <Link to="/my-bookings" className="nav__link">
              Мои бронирования
            </Link>
          )}
        </nav>
        <div className="header__actions">
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
                Личный кабинет
              </Link>
              <Link to="/rooms" className="header__cta">
                Забронировать
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
};

export default Header;

