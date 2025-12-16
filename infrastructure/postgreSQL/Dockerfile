FROM postgres:16-alpine

RUN apk add --no-cache postgresql16-contrib

COPY postgresql.conf /etc/postgresql/postgresql.conf
COPY pg_hba.conf /etc/postgresql/pg_hba.conf

COPY migrations/1_init.up.sql /docker-entrypoint-initdb.d/

RUN chown -R postgres:postgres /etc/postgresql/

CMD [ "postgres", "-c", "config_file=/etc/postgresql/postgresql.conf" ]

EXPOSE 5432

HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
    CMD pg_isready -U ${POSTGRES_USER:-booking_user} -d ${POSTGRES_DB:-booking} || exit 1
