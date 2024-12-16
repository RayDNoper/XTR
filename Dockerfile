FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace/app

COPY gradlew .
COPY gradlew.bat .
COPY gradle gradle
COPY build.gradle .
COPY src src
COPY .env .env
COPY DSL DSL

RUN chmod 754 ./gradlew
RUN ./gradlew -Pprod clean bootJar
RUN mkdir -p build/libs && (cd build/libs; jar -xf *.jar)

FROM eclipse-temurin:17-jdk
VOLUME /build/tmp

ARG DEPENDENCY=/workspace/app/build/libs
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
COPY DSL /DSL

ENV application.dslPath=/DSL

COPY .env /app/.env
RUN echo BUILDTIME=`date +%s` >> /app/.env

RUN useradd xtr
RUN chown -R xtr:xtr /app
RUN chown -R xtr:xtr /DSL
USER xtr

EXPOSE 9010

ENTRYPOINT ["java","-cp","app:app/lib/*","ee.buerokratt.xtr.XTRApplication"]
