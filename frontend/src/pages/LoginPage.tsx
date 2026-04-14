import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isLoading } = useAuthStore();
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    remember: false,
  });
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!formData.email || !formData.password) {
      setError('Пожалуйста, заполните все поля');
      return;
    }

    try {
      await login(formData.email, formData.password);
      const from = (location.state as any)?.from?.pathname || '/';
      navigate(from, { replace: true });
    } catch (err: any) {
      setError(
        err.response?.data?.message ||
        err.message ||
        'Неверный email или пароль. Проверьте правильность введенных данных.'
      );
    }
  };

  return (
    <div className="login">
      <div className="login__image" aria-hidden="true">
        <img src="/assets/main.jpg" alt="Главный корпус Политеха" />
      </div>
      <div className="login__content">
        <div className="login__brand">
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
            <span className="brand__tagline">Система бронирования аудиторий</span>
          </div>
        </div>
        <form className="login-card" onSubmit={handleSubmit}>
          <h1>Вход в систему</h1>
          <p>Используйте корпоративную учетную запись университета.</p>
          
          {error && <div className="error" style={{ marginBottom: '1rem' }}>{error}</div>}

          <label className="login-card__field">
            <span>Логин (почта @spbstu.ru)</span>
            <input
              type="email"
              name="email"
              placeholder="name@spbstu.ru"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              required
            />
          </label>
          <label className="login-card__field">
            <span>Пароль</span>
            <input
              type="password"
              name="password"
              placeholder="Введите пароль"
              value={formData.password}
              onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              required
            />
          </label>
          <label className="login-card__checkbox">
            <input
              type="checkbox"
              name="remember"
              checked={formData.remember}
              onChange={(e) => setFormData({ ...formData, remember: e.target.checked })}
            />
            <span>Запомнить устройство</span>
          </label>
          <button 
            type="submit" 
            className="btn btn--primary login-card__submit"
            disabled={isLoading}
          >
            {isLoading ? 'Вход...' : 'Войти'}
          </button>
          <div className="login-card__links">
            <a href="#">Забыли пароль?</a>
            <Link to="/">Перейти к бронированию</Link>
          </div>
        </form>
      </div>
    </div>
  );
};

export default LoginPage;
