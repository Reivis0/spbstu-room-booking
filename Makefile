# Project Name
PROJECT_NAME := spbstu-room-booking
COMPOSE_FILE := docker-compose.yml
ENV_FILE := .env

# BuildKit is mandatory for the optimized Dockerfiles
export DOCKER_BUILDKIT := 1
export COMPOSE_DOCKER_CLI_BUILD := 1

.PHONY: all build up down stop restart ps logs clean infra api engine ui migrate-up migrate-create help

# Load environment variables
ifneq (,$(wildcard $(ENV_FILE)))
include $(ENV_FILE)
export
endif

# Default target
all: build up

## Build & Run
build: $(ENV_FILE) ## Build all services using parallel build
	docker compose -f $(COMPOSE_FILE) build --parallel

up: $(ENV_FILE) ## Start all services in background
	docker compose -f $(COMPOSE_FILE) up -d

down: ## Stop and remove all containers, networks
	docker compose -f $(COMPOSE_FILE) down

stop: ## Stop all containers without removing them
	docker compose -f $(COMPOSE_FILE) stop

restart: ## Restart all services
	docker compose -f $(COMPOSE_FILE) restart

ps: ## List running containers
	docker compose -f $(COMPOSE_FILE) ps

logs: ## Follow logs from all services
	docker compose -f $(COMPOSE_FILE) logs -f --tail=100

clean: ## Remove containers, volumes, and networks
	docker compose -f $(COMPOSE_FILE) down -v
	docker system prune -f --volumes

## Partial Management (For faster development)
infra: $(ENV_FILE) ## Start only infrastructure (DB, Redis, NATS)
	docker compose up -d postgres redis nats migrate

api: $(ENV_FILE) ## Rebuild and restart only the Java API
	docker compose build booking_api
	docker compose up -d --no-deps booking_api

engine: $(ENV_FILE) ## Rebuild and restart only the C++ Engine
	docker compose build availabilityengine
	docker compose up -d --no-deps availabilityengine

ui: $(ENV_FILE) ## Rebuild and restart only the Frontend
	docker compose build frontend
	docker compose up -d --no-deps frontend

## Database Migrations
FLYWAY_CMD = docker run --rm \
	--network=$(PROJECT_NAME)_booking-net \
	-v $(PWD)/infrastructure/postgreSQL/migrations:/flyway/sql:ro \
	flyway/flyway:10.10.0 \
	-url=jdbc:postgresql://postgres:5432/$(POSTGRES_DB) \
	-user=$(POSTGRES_USER) \
	-password=$(POSTGRES_PASSWORD) \
	-locations=filesystem:/flyway/sql \
	-table=flyway_schema_history

migrate-up: ## Run flyway migrations manually
	$(FLYWAY_CMD) migrate

migrate-create: ## Create a new migration file (Vxxxx__name.sql)
	@printf "Migration name (snake_case): "; \
	read name; \
	count=$$(find infrastructure/postgreSQL/migrations -maxdepth 1 -name 'V*.sql' | wc -l); \
	version=$$(printf "%06d" $$(( $$count + 1 ))); \
	touch infrastructure/postgreSQL/migrations/V$${version}__$${name}.sql; \
	touch infrastructure/postgreSQL/migrations/U$${version}__$${name}.sql; \
	echo "Created V$${version}__$${name}.sql and U$${version}__$${name}.sql"

## Helpers
$(ENV_FILE):
	@if [ ! -f $(ENV_FILE) ]; then \
		echo "Creating .env from .env.example..."; \
		cp .env.example $(ENV_FILE); \
	fi

help: ## Show this help message
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'
