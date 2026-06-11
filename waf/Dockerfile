FROM owasp/modsecurity-crs:nginx-alpine

USER root

# Create the dir where the mkcert cert is mounted at runtime
RUN mkdir -p /etc/nginx/conf

# Override the default.conf template so our rate limiting + routing survive
# the entrypoint's envsubst pass at container startup
COPY default.conf.template /etc/nginx/templates/conf.d/default.conf.template

# ModSecurity tuning (CRS exclusions, paranoia level, thresholds)
COPY modsecurity.conf     /etc/nginx/modsec/modsecurity.conf
COPY crs-setup.d/         /etc/nginx/modsec/crs-setup.d/

USER nginx

EXPOSE 80 443
