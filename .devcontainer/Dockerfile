FROM mcr.microsoft.com/devcontainers/base:bookworm@sha256:85a349062e4ccc4755304f1264183e604391697608765e72765340b7cb7e2438

# Java AWT image-generation tests require FreeType, Fontconfig, and an installed font.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libfreetype6 \
        fontconfig \
        fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

