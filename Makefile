.PHONY: up down build logs ps

DC_FILE := infrastructure/docker-compose.yml
ENV_FILE := infrastructure/.env

up:
	docker-compose -f $(DC_FILE) --env-file $(ENV_FILE) up -d --build

down:
	docker-compose -f $(DC_FILE) --env-file $(ENV_FILE) down

logs:
	docker-compose -f $(DC_FILE) --env-file $(ENV_FILE) logs -f

ps:
	docker-compose -f $(DC_FILE) --env-file $(ENV_FILE) ps
