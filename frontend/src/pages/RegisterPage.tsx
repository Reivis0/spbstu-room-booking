import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import UniversitySelect from '../features/university/ui/UniversitySelect';
import { useUniversitySearchParam } from '../shared/university/useUniversitySearchParam';

const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { universityCode, setUniversityCode } = useUniversitySearchParam();
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    repeatPassword: '',
    accepted: false,
  });
  const [error, setError] = useState<string | null>(null);

  const redirectAfterAuth = () => {
    const from = (location.state as any)?.from?.pathname || `/login?university=${universityCode}`;
    navigate(from, { replace: true });
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);

    if (!formData.fullName || !formData.email || !formData.password || !formData.repeatPassword) {
      setError('Заполните все поля регистрации');
      return;
    }

    if (formData.password !== formData.repeatPassword) {
      setError('Пароли не совпадают');
      return;
    }

    if (!formData.accepted) {
      setError('Подтвердите правила обработки данных');
      return;
    }

    localStorage.setItem(
      'pendingRegistration',
      JSON.stringify({
        fullName: formData.fullName,
        email: formData.email,
        university: universityCode,
        createdAt: new Date().toISOString(),
      })
    );

    redirectAfterAuth();
  };

  return (
    <div className="login register-page">
      <div className="login__image" aria-hidden="true">
        <img src="/assets/main.jpg" alt="Учебное пространство университета" />
      </div>
      <div className="login__content">
        <div className="login__brand">
          <div className="brand__logo" aria-hidden="true">
            PB
          </div>
          <div>
            <span className="brand__name">POLYBOOK</span>
            <span className="brand__tagline">Учебные пространства Петербурга</span>
          </div>
        </div>

        <form className="login-card register-card register-card--auth" onSubmit={handleSubmit}>
          <h1>Создать аккаунт</h1>
          <p>Заполните профиль, выберите университет и получите доступ к бронированию аудиторий.</p>

          {error && <div className="error error--compact">{error}</div>}

          <UniversitySelect
            id="register-university"
            label="Вуз"
            value={universityCode}
            onChange={setUniversityCode}
          />

          <label className="register-card__field">
            <span>ФИО</span>
            <input
              type="text"
              name="fullName"
              placeholder="Иванова Анна Сергеевна"
              value={formData.fullName}
              onChange={(event) => setFormData({ ...formData, fullName: event.target.value })}
              required
            />
          </label>

          <label className="register-card__field">
            <span>Корпоративная почта</span>
            <input
              type="email"
              name="email"
              placeholder="name@university.ru"
              value={formData.email}
              onChange={(event) => setFormData({ ...formData, email: event.target.value })}
              required
            />
          </label>

          <label className="register-card__field">
            <span>Пароль</span>
            <input
              type="password"
              name="password"
              placeholder="Минимум 8 символов"
              value={formData.password}
              onChange={(event) => setFormData({ ...formData, password: event.target.value })}
              required
            />
          </label>

          <label className="register-card__field">
            <span>Повторите пароль</span>
            <input
              type="password"
              name="repeatPassword"
              placeholder="Повторите пароль"
              value={formData.repeatPassword}
              onChange={(event) => setFormData({ ...formData, repeatPassword: event.target.value })}
              required
            />
          </label>

          <label className="register-card__field register-card__field--checkbox">
            <input
              type="checkbox"
              checked={formData.accepted}
              onChange={(event) => setFormData({ ...formData, accepted: event.target.checked })}
            />
            <span>Согласна с правилами бронирования и обработкой заявки</span>
          </label>

          <button type="submit" className="btn btn--primary login-card__submit">
            Зарегистрироваться
          </button>

          <div className="login-card__links">
            <Link to="/login">Уже есть аккаунт</Link>
            <Link to="/">На главную</Link>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RegisterPage;
