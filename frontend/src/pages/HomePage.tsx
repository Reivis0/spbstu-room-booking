import React from 'react';
import { Link } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';

const HomePage: React.FC = () => {
  const { isAuthenticated } = useAuthStore();

  return (
    <>
      <main>
        <section className="hero" id="hero">
          <div className="container hero__content">
            <div className="hero__text">
              <p className="hero__eyebrow">Система бронирования аудиторий</p>
              <h1 className="hero__title">
                Бронируйте аудитории для занятий, семинаров и мероприятий
              </h1>
              <p className="hero__subtitle">
                Найдите подходящую аудиторию по параметрам, забронируйте время и управляйте своими бронированиями. 
                Простой и удобный интерфейс для быстрого поиска и бронирования.
              </p>
              <div className="hero__actions">
                <Link to="/rooms" className="btn btn--primary">
                  Найти аудиторию
                </Link>
                {isAuthenticated && (
                  <Link to="/my-bookings" className="btn btn--ghost">
                    Мои бронирования
                  </Link>
                )}
                {!isAuthenticated && (
                  <Link to="/login" className="btn btn--ghost">
                    Войти в систему
                  </Link>
                )}
              </div>
            </div>
            <div className="hero__card">
              <h2>Возможности системы</h2>
              <ul className="booking-list">
                <li>
                  <span className="booking-list__title">Поиск аудиторий</span>
                  <span className="booking-list__meta">Фильтрация по корпусу, этажу и вместимости</span>
                </li>
                <li>
                  <span className="booking-list__title">Просмотр оборудования</span>
                  <span className="booking-list__meta">Информация о проекторах, досках, компьютерах и Wi-Fi</span>
                </li>
                <li>
                  <span className="booking-list__title">Бронирование</span>
                  <span className="booking-list__meta">Выберите дату и время для вашего мероприятия</span>
                </li>
                <li>
                  <span className="booking-list__title">Управление бронированиями</span>
                  <span className="booking-list__meta">Просмотр, отмена и отслеживание ваших бронирований</span>
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
                Система бронирования аудиторий позволяет быстро найти и забронировать подходящую аудиторию 
                для ваших занятий, семинаров или мероприятий.
              </p>
              <ul className="feature-list">
                <li><strong>Поиск аудиторий:</strong> Используйте фильтры для поиска по корпусу, этажу и вместимости</li>
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
              <p>Отдел бронирования аудиторий</p>
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
              <h3>Как нас найти</h3>
              <p>Санкт-Петербург, Политехническая ул., 29</p>
              <p>Главный корпус, аудитория 105, стойка выдачи ключей</p>
              <p className="contacts__note">Работаем ежедневно, подробности по телефону.</p>
            </div>
          </div>
        </section>
      </main>

      <footer className="footer">
        <div className="container footer__inner">
          <span>© 2025 СПбГТУ. Главный корпус</span>
          <a href="#" className="footer__link">
            Политика конфиденциальности
          </a>
        </div>
      </footer>
    </>
  );
};

export default HomePage;
