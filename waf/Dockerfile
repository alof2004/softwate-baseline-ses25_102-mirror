FROM owasp/modsecurity-crs:nginx-alpine-202501130

# Generate a self-signed TLS certificate for the lab environment.
# In production this would be replaced by a CA-signed cert via a secret or volume mount.
RUN apk add --no-cache openssl && \
    mkdir -p /etc/nginx/tls && \
    openssl req -x509 -nodes -days 365 \
        -subj "/C=PT/ST=Aveiro/O=ClinicLab/CN=localhost" \
        -newkey rsa:2048 \
        -keyout /etc/nginx/tls/server.key \
        -out    /etc/nginx/tls/server.crt

COPY nginx.conf          /etc/nginx/nginx.conf
COPY modsecurity.conf    /etc/nginx/modsec/modsecurity.conf
COPY crs-setup.d/        /etc/nginx/modsec/crs-setup.d/

EXPOSE 80 443
