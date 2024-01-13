JAVAC = javac
JAR = jar
SRC_FOLDER = src
OUTPUT_FOLDER = out
JAR_FILE = peerProcess.jar
MAIN_CLASS = main.java.peerProcess
JAVAC_FLAGS = -d $(OUTPUT_FOLDER)
clean:
	rm -rf $(OUTPUT_FOLDER) $(JAR_FILE)
compile:
	$(JAVAC) $(JAVAC_FLAGS) $(shell find $(SRC_FOLDER) -name "*.java")
jar: compile
	$(JAR) cfe $(JAR_FILE) $(MAIN_CLASS) -C $(OUTPUT_FOLDER) .

.DEFAULT_GOAL := jar
