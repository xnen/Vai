# Vai Project

![Project Logo](./images/logo.png)

## Introduction

Vai facilitates quick iterative workflows for your projects using `o1-preview` or `o1-mini`. It is an AI-assisted IDE designed for rapid development cycles. To use Vai, an OpenAI API key is required. Vai also supports Gemini models, which require a Google API Key.

**Warning:** This is a new release that can overwrite existing files. It is highly recommended to back up your work before using Vai. While it does create backups of every file it modifies, edge cases may still occur.

## Installation

### Prerequisites

- **Java Development Kit (JDK 11 or higher):** Ensure that Java is installed on your system. You can download it from [Oracle's official website](https://www.oracle.com/java/technologies/javase-downloads.html).
- **Maven:** This project uses Maven for dependency management. You can download it from [Maven's official website](https://maven.apache.org/download.cgi).
- **Python 3.x:** Required for Gemini model support. Ensure Python 3 is installed and accessible in your system's PATH.
- **pip:** Python package installer, usually comes with Python installations.

### Installing Python Dependencies for Gemini (Optional)

If you intend to use Gemini models, you need to install the required Python package. Navigate to the `vai` project directory in your terminal and run:

```bash
pip install -r requirements.txt
```

This will install the `google-generativeai` library, which is necessary for interacting with Gemini models.

### Installing Meld

#### On Windows:

(Windows support is untested. Proceed with caution.)

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

    Ensure that Maven and JDK 11 are installed. Navigate to the project directory and run:

    ```bash
    mvn clean package
    ```

    Alternatively, in IntelliJ, use a JDK 11 and run `maven package` from the IDE.

    This command will compile the project into a fat JAR file, resulting in the `target` directory.

3. **Configure API Keys:**

    - **OpenAI API Key:**
        - Launch the application:

        ```bash
        java -jar VaiProject-1.0.0.jar
        ```

        - Go to `Config` > `Configure...` in the menu.
        - Enter your OpenAI API Key and save.
        - Restart Vai to ensure the provider initializes.

        Alternatively, you can manually add your API key to the `openai-api-key.dat` file located in your user's home directory.

    - **Google API Key (for Gemini):**
        - Set the `GOOGLE_API_KEY` environment variable in your operating system.
          - **On Linux/macOS:** Add `export GOOGLE_API_KEY='YOUR_API_KEY'` to your `.bashrc`, `.zshrc`, or similar shell configuration file. Replace `YOUR_API_KEY` with your actual Google API key.
          - **On Windows:**  Set a system environment variable named `GOOGLE_API_KEY` with your API key as the value. You can do this through the System Properties dialog.

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
    - Select the desired OpenAI model (`o1-mini` or `o1-preview`) or Gemini model (`gemini-2.0-flash-thinking-exp-01-21`) from the dropdown.
    - Click `Submit` to send the request.
    - The application will handle the response, update files, and create backups as necessary.
    - Each response shows all diffs using Meld, which allow you to merge or fix AI-generated changes.
