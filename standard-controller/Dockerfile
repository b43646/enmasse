FROM enmasseproject/java-base:8-7

ARG version=latest
ENV VERSION ${version}
ADD target/standard-controller-${VERSION}.jar /standard-controller.jar

CMD ["/opt/run-java/launch_java.sh", "/standard-controller.jar"]
