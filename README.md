# Vai Project
Placeholder o1-mini written README.

![Project Logo](./images/logo.png)

## Introduction

Vai allows quick iterative workflows for your projects using o1-preview or o1-mini.
Just another AI-created slop IDE for quick iteration. Must have an OpenAI key.

This is BRAND NEW and OVERWRITES FILES. It's recommended to backup anything you're doing beforehand!
It does make backups of every file it makes, but I expect edge cases at this stage.
It has like 4 hours of tired heavily AI-influenced development at stage.

## Installation

### Prerequisites

- **Java Development Kit (JDK) 11 (15?) or higher:** Ensure that Java is installed on your system. You can download it from [Oracle's official website](https://www.oracle.com/java/technologies/javase-downloads.html).
- **Maven:** This project uses Maven for dependency management. You can download it from [Maven's official website](https://maven.apache.org/download.cgi).

### Installing Meld

#### On Windows:

(Windows probably won't work at all, but maybe. Perhaps. Untested.)

1. Download the Meld installer from the [official website](https://meldmerge.org/).
2. Run the installer and follow the on-screen instructions.

#### On macOS:

You can install Meld using Homebrew:

```bash
brew install --cask meld
```

#### On Linux:

For Debian-based distributions:

```bash
sudo apt-get update
sudo apt-get install meld
```

For Fedora:

```bash
sudo dnf install meld
```

### Setup

1. **Clone the Repository:**

    ```bash
    git clone https://github.com/xnen/vai.git
    cd vai
    ```

2. **Install Dependencies and Build the Project:**

    Ensure that Maven is installed. Navigate to the project directory and run:

    ```bash
    mvn clean package
    ```

    This command will compile the project into a fat JAR file. Hopefully!
    It should result in the `target` directory.

3. **Configure API Key:**

    - Launch the application:

    ```bash
    java -jar VaiProject-1.0.0.jar
    ```

    - Go to `Config` > `Configure...` in the menu.
    - Enter your OpenAI API Key and save.

    Alternatively, you can manually add your API key to the `openai-api-key.dat` file located in your user's home directory.

## Usage

1. **Opening a Workspace:**

    - Navigate to `File` > `Open Directory...` to select your project's workspace directory.
    - The project structure will be displayed in the `Project Panel`.

2. **Managing Active Files:**

    - Use the `Active Files Panel` to view and manage files marked as active.
    - Right-click on files to perform actions like deleting from active files.

3. **Viewing File Contents:**

    - Select a file from the `Project Panel` to view its contents in the `File Viewer` with syntax highlighting.

4. **Submitting Requests to OpenAI:**

    - Select any relevant files in the project viewer that the LLM should see.
    - Enter your request in the text area at the bottom of the application.
    - Select the desired OpenAI model (`o1-mini` or `o1-preview`) from the dropdown.
    - Click `Submit` to send the request.
    - The application will handle the response, update files, and create backups as necessary.
    - Each response shows all diffs using Meld, which allow you to merge in whatever or fix AI mistakes.