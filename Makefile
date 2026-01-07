.PHONY: build test run all up down logs ps clean

COMPOSE_FILE := docker-compose.yml
ENV_FILE := .env

build:
	@[ -f $(ENV_FILE) ] || cp .env.example $(ENV_FILE) 2>/dev/null || true
	docker-compose -f $(COMPOSE_FILE) build --no-cache --pull

test:
	@docker-compose -f $(COMPOSE_FILE) run --rm booking-api mvn test || true

run:
	@[ -f $(ENV_FILE) ] || cp .env.example $(ENV_FILE) 2>/dev/null || true
	@docker ps -a --filter "name=booking-" --format "{{.Names}}" | xargs -r docker rm -f 2>/dev/null || true
	docker-compose -f $(COMPOSE_FILE) up -d

all: build test run

up:
	@[ -f $(ENV_FILE) ] || cp .env.example $(ENV_FILE) 2>/dev/null || true
	@docker ps -a --filter "name=booking-" --format "{{.Names}}" | xargs -r docker rm -f 2>/dev/null || true
	docker-compose -f $(COMPOSE_FILE) up -d --build

down:
	docker-compose -f $(COMPOSE_FILE) down

logs:
	docker-compose -f $(COMPOSE_FILE) logs -f

ps:
	docker-compose -f $(COMPOSE_FILE) ps

clean:
	docker-compose -f $(COMPOSE_FILE) down -v
	docker network prune -f
