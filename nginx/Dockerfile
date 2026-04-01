FROM nginx:alpine

COPY nginx.conf /etc/nginx/nginx.conf

RUN mkdir -p /usr/share/nginx/html/frontend /usr/share/nginx/html/dashboard

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]