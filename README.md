<div align="center">
  <img src="images/VAI-LOGO.png" alt="VAI Logo" width="130" height="130" style="margin-bottom: -30px;" />
  <h1>Vai Project - Java</h1>
  <p>
    <a href="#">Official Website</a> |
    <a href="#">Documentation</a> |
    <a href="#">Community</a>
  </p>
</div>


## Intro

Welcome to **Vai**, AI slop dev tool written in Java.


## Some Features ✨

- Generate Code 
- Choose from various models like OpenAI’s `o1`, `o1-preview`, `o1-mini`, Gemini, and more.
- **Audio & Voice Integration:** Record audio prompts and convert them into actionable code changes on some models.
- **Plugin Architecture:** Extend functionality with versatile plugins for running shell commands, displaying messages, and more.
- **Workspace Awareness:** Effortlessly switch between projects while retaining context and settings across sessions.


## Quickstart 🚀

### Prerequisites
- **Java 11+**  
  [Download from Oracle](https://www.oracle.com/java/technologies/javase-downloads.html)
- **Maven**  
  [Download Maven](https://maven.apache.org/download.cgi)
- **Python 3.x & pip:** Required for Gemini support.
- **Meld:** For viewing file diffs  
  - **Linux (Debian):** `sudo apt-get update && sudo apt-get install meld`  
  - **Linux (Fedora):** `sudo dnf install meld`  
  - **macOS:** `brew install --cask meld`  
  - **Windows:** [Download Installer](https://meldmerge.org/)

### Installation

1. **Clone the Repository:**
    ```bash
    git clone https://github.com/xnen/vai.git
    cd vai
    ```

2. **Build the Project:**
    ```bash
    mvn clean package
    ```
    This compiles the project and creates a fat JAR package in the `target` directory.

3. **Configure API Keys:**
   - **OpenAI API Key:** Launch Vai, navigate to `Config > Configure...`, and enter your API key.
   - **Google API Key (for Gemini):** Set the `GOOGLE_API_KEY` environment variable in your system.

4. **Run Vai:**
    ```bash
    java -jar target/VaiProject-1.0.0.jar
    ```

---

## Gemini Client Setup (Python) 🐍

The Gemini client is a dependency to run the Gemini API, since I haven't found any Java SDK for Google's GenAI. So you'll need these to run the gemini models.
### Prerequisites

- **Python 3.x**  
- **pip**

### Steps

1. **Install Python Dependencies:**
    Navigate to the root of the project and run:
    ```bash
    pip install -r requirements.txt
    ```

2. **Set the Google API Key:**
    Make sure the environment variable `GOOGLE_API_KEY` is set. For example, on Linux/macOS:
    ```bash
    export GOOGLE_API_KEY="your_google_api_key_here"
    ```
    On Windows (CMD):
    ```cmd
    set GOOGLE_API_KEY=your_google_api_key_here
    ```

    Prior to running the application.
---

## News & Updates 📰

Stay in the loop with the latest from Vai:

- **Version 1.0.0 Released:** Initial release featuring AI integration and dynamic workspace management.

Follow us on social media (none of these are real):
- **Website:** [example.com](#)
- **Blog:** [blog.example.com](#)
- **Twitter:** [@vai_project](#)


## Shoutouts & Acknowledgements 🙏

A heartfelt thanks to:
- **OpenAI Java SDK:** For powering our AI integrations.
- **FlatLaf:** Delivering a modern UI design.
- **RSyntaxTextArea & AutoComplete:** Enhancing the code editing experience.
- **Meld:** Providing an indispensable diffing tool.
- **And many more:** We rely on the amazing work of countless open source contributors to bring Vai to life!

## Contributing 💡

We welcome your contributions! Check our [TODOs.txt](TODOs.txt) for current issues and upcoming tasks. Your ideas, code, and feedback are always appreciated. Let's build something amazing together!

Happy Coding! 👩‍💻👨‍💻  
