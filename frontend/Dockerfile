# Wishka frontend image: static SPA served by unprivileged nginx. Expects `npm run build` output in dist/.
FROM nginxinc/nginx-unprivileged:alpine
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY dist/ /usr/share/nginx/html/
EXPOSE 8080
