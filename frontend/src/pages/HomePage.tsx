import React from 'react';
import { Link } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import UniversitySelect from '../features/university/ui/UniversitySelect';
import { useUniversitySearchParam } from '../shared/university/useUniversitySearchParam';
import { DEFAULT_UNIVERSITY, getUniversity } from '../shared/university/universities';

const HomePage: React.FC = () => {
  const { isAuthenticated } = useAuthStore();
  const { universityCode, setUniversityCode } = useUniversitySearchParam();
  const selectedUniversity = getUniversity(universityCode);
  const universityQuery = universityCode === DEFAULT_UNIVERSITY ? '' : `?university=${universityCode}`;

  return (
    <>
      <main>
        <section className="hero" id="hero">
          <div className="container hero__content">
            <div className="hero__text">
              <p className="hero__eyebrow">Единый сервис учебных пространств Санкт-Петербурга</p>
              <h1 className="hero__title">
                POLYBOOK
              </h1>
              <p className="hero__subtitle">
                Платформа для быстрого бронирования аудиторий в разных университетах.
                Выберите СПбПУ, СПбГУ или ЛЭТИ, найдите подходящее помещение и управляйте
                бронированиями в одном интерфейсе.
              </p>
              <div className="hero__university">
                <UniversitySelect
                  id="home-university"
                  label="Выберите вуз"
                  value={universityCode}
                  onChange={setUniversityCode}
                />
                <span className="hero__university-note">
                  Сейчас выбран {selectedUniversity.label}. Поиск и календарь откроются с этим фильтром.
                </span>
              </div>
              <div className="hero__actions">
                {isAuthenticated ? (
                  <>
                    <Link to={`/rooms${universityQuery}`} className="btn btn--primary">
                      Найти аудиторию
                    </Link>
                    <Link to={`/my-bookings${universityQuery}`} className="btn btn--ghost">
                      Мои бронирования
                    </Link>
                  </>
                ) : (
                  <>
                    <Link to={`/register${universityQuery}`} className="btn btn--primary">
                      Зарегистрироваться
                    </Link>
                    <Link to="/login" className="btn btn--ghost">
                      Войти в систему
                    </Link>
                  </>
                )}
              </div>
            </div>
            <div className="hero__card">
              <h2>Что уже есть в POLYBOOK</h2>
              <ul className="booking-list">
                <li>
                  <span className="booking-list__title">Три университета</span>
                  <span className="booking-list__meta">СПбПУ, СПбГУ и ЛЭТИ с отдельными корпусами и аудиториями</span>
                </li>
                <li>
                  <span className="booking-list__title">Фильтр по вузу</span>
                  <span className="booking-list__meta">Поиск и календарь показывают данные только выбранного университета</span>
                </li>
                <li>
                  <span className="booking-list__title">Real-time статусы</span>
                  <span className="booking-list__meta">Карточки обновляются по событиям бронирований без перезагрузки</span>
                </li>
                <li>
                  <span className="booking-list__title">Мои бронирования</span>
                  <span className="booking-list__meta">Вкладки по вузам помогают быстро найти нужную бронь</span>
                </li>
              </ul>
            </div>
          </div>
        </section>

        <section id="about" className="about">
          <div className="container about__grid">
            <div>
              <h2>Как использовать систему</h2>
              <p>
                POLYBOOK помогает быстро найти и забронировать подходящую аудиторию для занятий,
                семинаров или мероприятий в университетах Санкт-Петербурга.
              </p>
              <ul className="feature-list">
                <li><strong>Выбор вуза:</strong> Сначала выберите СПбПУ, СПбГУ или ЛЭТИ</li>
                <li><strong>Поиск аудиторий:</strong> Используйте фильтры по корпусу, этажу и вместимости</li>
                <li><strong>Просмотр информации:</strong> Узнайте о доступном оборудовании в каждой аудитории</li>
                <li><strong>Бронирование:</strong> Выберите дату и время, создайте бронирование в несколько кликов</li>
                <li><strong>Управление:</strong> Просматривайте все свои бронирования и отменяйте при необходимости</li>
              </ul>
              <p style={{ marginTop: '1.5rem' }}>
                Для начала работы необходимо войти в систему, используя корпоративную учетную запись университета.
              </p>
            </div>
            <aside className="about__card">
              <h3>Быстрый старт</h3>
              <ol style={{ margin: '0', paddingLeft: '1.5rem', display: 'grid', gap: '0.75rem' }}>
                <li>Войдите в систему через "Личный кабинет"</li>
                <li>Перейдите в раздел "Аудитории"</li>
                <li>Используйте фильтры для поиска</li>
                <li>Нажмите "Забронировать" на нужной аудитории</li>
                <li>Выберите дату и время</li>
                <li>Подтвердите бронирование</li>
              </ol>
            </aside>
          </div>
        </section>

        <section id="contacts" className="contacts">
          <div className="container contacts__content">
            <div>
              <h2>Контакты</h2>
              <p>Служба поддержки POLYBOOK</p>
              <ul className="contacts__list">
                <li>
                  <span>Email:</span>
                  <a href="mailto:audit@spbstu.ru">audit@spbstu.ru</a>
                </li>
                <li>
                  <span>Телефон:</span>
                  <a href="tel:+78125555555">+7 (812) 555-55-55</a>
                </li>
                <li>
                  <span>Телеграм:</span>
                  <a href="https://t.me/unic_audit" target="_blank" rel="noopener noreferrer">
                    t.me/unic_audit
                  </a>
                </li>
              </ul>
            </div>
            <div className="contacts__card">
              <h3>Где работает сервис</h3>
              <p>Санкт-Петербург: СПбПУ, СПбГУ и ЛЭТИ</p>
              <p>Единый интерфейс для бронирования учебных пространств разных университетов.</p>
              <p className="contacts__note">Подробности по доступу и поддержке можно уточнить у диспетчера.</p>
            </div>
          </div>
        </section>
      </main>

      <footer className="footer">
        <div className="container footer__inner">
          <span>© 2026 POLYBOOK</span>
          <a href="/privacy" className="footer__link">
            Политика конфиденциальности
          </a>
        </div>
      </footer>
    </>
  );
};

export default HomePage;
