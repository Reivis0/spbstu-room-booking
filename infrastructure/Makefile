COMPOSE_FILE := docker-compose.yml
ENV_FILE := .env

build:
	docker-compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) build

up:
	docker-compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) up

down:
	docker-compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) down

ps:
	docker-compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) ps

logs:
	docker-compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) logs

