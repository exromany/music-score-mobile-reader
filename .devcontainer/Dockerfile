FROM --platform=linux/amd64 mcr.microsoft.com/devcontainers/java:17

# Android SDK versions
ARG ANDROID_SDK_VERSION=34
ARG ANDROID_BUILD_TOOLS_VERSION=34.0.0
ARG ANDROID_CMD_LINE_TOOLS_VERSION=11076708

# Set environment variables
ENV ANDROID_HOME=/usr/local/android-sdk
ENV ANDROID_SDK_ROOT=${ANDROID_HOME}
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# Install Android SDK
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    cd ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_CMD_LINE_TOOLS_VERSION}_latest.zip -O cmdline-tools.zip && \
    unzip -q cmdline-tools.zip && \
    rm cmdline-tools.zip && \
    mv cmdline-tools latest

# Accept licenses and install SDK components
RUN yes | sdkmanager --licenses && \
    sdkmanager \
        "platform-tools" \
        "platforms;android-${ANDROID_SDK_VERSION}" \
        "build-tools;${ANDROID_BUILD_TOOLS_VERSION}"

# Set proper permissions
RUN chmod -R a+rwx ${ANDROID_HOME}
