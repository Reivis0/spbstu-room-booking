FROM nginx:1.25-alpine

RUN rm /etc/nginx/conf.d/default.conf

COPY nginx.conf /etc/nginx/nginx.conf
COPY conf.d/ /etc/nginx/conf.d/

RUN mkdir -p /var/log/nginx /var/cache/nginx/client_temp

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]

HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
  CMD curl -f http://localhost/health || exit 1