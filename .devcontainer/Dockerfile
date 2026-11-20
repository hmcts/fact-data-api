FROM mcr.microsoft.com/devcontainers/base:bookworm@sha256:06701e75787b0b948709e803f95b42cabf37607b1c308c57471d6f0252149d87

# Java AWT image-generation tests require FreeType, Fontconfig, and an installed font.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libfreetype6 \
        fontconfig \
        fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

