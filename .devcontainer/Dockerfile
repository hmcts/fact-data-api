FROM mcr.microsoft.com/devcontainers/base:bookworm@sha256:9a5942e64b35c5497c591b9bf177f4a3002d43440bf0a5e1a14fc34667b1a5c1

# Java AWT image-generation tests require FreeType, Fontconfig, and an installed font.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libfreetype6 \
        fontconfig \
        fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

