.PHONY: build test run all up down logs ps clean migrate-up migrate-info migrate-validate migrate-repair migrate-undo migrate-create

COMPOSE_FILE := docker-compose.yml
ENV_FILE := .env

ifneq (,$(wildcard $(ENV_FILE)))
include $(ENV_FILE)
export
endif

FLYWAY = docker run --rm \
	--network=$$(docker compose ps -q postgres | xargs docker inspect --format='{{range .NetworkSettings.Networks}}{{.NetworkID}}{{end}}') \
	-v $(PWD)/infrastructure/postgreSQL/migrations:/flyway/sql:ro \
	flyway/flyway:10.10.0 \
	-url=jdbc:postgresql://postgres:5432/$(POSTGRES_DB) \
	-user=$(POSTGRES_USER) \
	-password=$(POSTGRES_PASSWORD) \
	-locations=filesystem:/flyway/sql \
	-table=flyway_schema_history

build:
	@[ -f $(ENV_FILE) ] || cp .env.example $(ENV_FILE) 2>/dev/null || true
	docker compose -f $(COMPOSE_FILE) build --parallel

test:
	@docker compose -f $(COMPOSE_FILE) run --rm booking-api mvn test || true

run:
	@[ -f $(ENV_FILE) ] || cp .env.example $(ENV_FILE) 2>/dev/null || true
	@docker ps -a --filter "name=booking-" --format "{{.Names}}" | xargs -r docker rm -f 2>/dev/null || true
	docker compose -f $(COMPOSE_FILE) up -d

all: build test run

up:
	@[ -f $(ENV_FILE) ] || cp .env.example $(ENV_FILE) 2>/dev/null || true
	@docker ps -a --filter "name=booking-" --format "{{.Names}}" | xargs -r docker rm -f 2>/dev/null || true
	docker compose -f $(COMPOSE_FILE) up --build -d

down:
	docker compose -f $(COMPOSE_FILE) down

logs:
	docker compose -f $(COMPOSE_FILE) logs -f

ps:
	docker compose -f $(COMPOSE_FILE) ps

clean:
	docker compose -f $(COMPOSE_FILE) down -v
	docker network prune -f

migrate-up:
	$(FLYWAY) migrate

migrate-info:
	$(FLYWAY) info

migrate-validate:
	$(FLYWAY) validate

migrate-repair:
	$(FLYWAY) repair

migrate-undo:
	$(FLYWAY) undo

migrate-create:
	@printf "Migration name (snake_case): "; \
	read name; \
	version=$$(printf "%06d" $$(( $$(find infrastructure/postgreSQL/migrations -maxdepth 1 -name 'V*.sql' | wc -l) + 1 ))); \
	touch infrastructure/postgreSQL/migrations/V$${version}__$${name}.sql; \
	touch infrastructure/postgreSQL/migrations/U$${version}__$${name}.sql; \
	echo "Created V$${version}__$${name}.sql and U$${version}__$${name}.sql"
