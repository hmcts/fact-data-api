FROM mcr.microsoft.com/devcontainers/base:bookworm@sha256:99f5250dd2d5f8b11046d5662dd94c1e3c1dae56b560c7caad5ceb7edf349046

# Java AWT image-generation tests require FreeType, Fontconfig, and an installed font.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libfreetype6 \
        fontconfig \
        fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

