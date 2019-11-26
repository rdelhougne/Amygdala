FROM oracle/graalvm-ce:latest
SHELL ["/bin/bash", "-c"]
RUN gu install native-image
RUN gu install python
RUN gu rebuild-images
