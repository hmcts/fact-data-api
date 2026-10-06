FROM mcr.microsoft.com/devcontainers/base:bookworm

# Java AWT image-generation tests require FreeType, Fontconfig, and an installed font.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libfreetype6 \
        fontconfig \
        fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

