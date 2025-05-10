import sys
import os
from google import genai
from google.genai import types
from google.api_core.exceptions import GoogleAPIError
import traceback

def main():
    api_key = os.environ.get("GOOGLE_API_KEY")
    if not api_key:
        print("Error: GOOGLE_API_KEY environment variable not set.", file=sys.stderr)
        sys.exit(1)

    model_name = sys.argv[1] if len(sys.argv) > 1 else "gemini-pro" # Default model
    prompt_file_path = sys.argv[2] if len(sys.argv) > 2 else None # Path to prompt file
    file_list_path = sys.argv[3] if len(sys.argv) > 3 else None # Path to file list

    prompt_text = ""
    if prompt_file_path:
        try:
            with open(prompt_file_path, 'r') as f:
                prompt_text = f.read()
        except FileNotFoundError:
            print(f"Error: Prompt file not found: {prompt_file_path}", file=sys.stderr)
            sys.exit(1)
        except Exception as e:
            print(f"Error reading prompt file {prompt_file_path}: {e}", file=sys.stderr)
            sys.exit(1)
    else:
        print("DO NOT.")
        sys.exit(1)

    client = genai.Client(api_key=api_key, http_options={'api_version':'v1alpha'})

    file_paths = []
    if file_list_path:
        try:
            with open(file_list_path, 'r') as f:
                file_paths = [line.strip() for line in f if line.strip()] # Read file paths from temp file
        except FileNotFoundError:
            print(f"Error: File list not found: {file_list_path}", file=sys.stderr)
            sys.exit(1)
        except Exception as e:
            print(f"Error reading file list {file_list_path}: {e}", file=sys.stderr)
            sys.exit(1)


    contents = []

    for file_path in file_paths:
        try:
            mime_type = get_mime_type(file_path)
            if mime_type:
                with open(file_path, 'rb') as f:
                    file_data = f.read()
                    contents.append(types.Part.from_bytes(data=file_data, mime_type=mime_type))
            else:
                print(f"Warning: Could not determine MIME type for file: {file_path}. Skipping file: {file_path}.", file=sys.stderr)
        except FileNotFoundError:
            print(f"Error: File not found: {file_path}", file=sys.stderr)
        except PermissionError:
            print(f"Error: Permission denied when reading file: {file_path}", file=sys.stderr)
        except Exception as e:
            print(f"Error reading file {file_path}: {e}", file=sys.stderr)

    contents.append(types.Part.from_text(prompt_text))

    try:
        response = client.models.generate_content(
            #model='gemini-2.0-flash-thinking-exp',
            model='gemini-2.5-pro-exp-03-25',
            contents=contents,
            config=types.GenerateContentConfig(
                thinking_config=types.ThinkingConfig(include_thoughts=False),
                temperature=0,
                top_p=0.95,
                top_k=20,
                candidate_count=1,
                seed=5,
                stop_sequences=["STOP!"],
                presence_penalty=0.0,
                frequency_penalty=0.0,
            )
        )
        print(response.text) # Print the text response to stdout
    except GoogleAPIError as api_error:
        print(f"Gemini API Error: {api_error}", file=sys.stderr)
        # Optionally print detailed traceback for API errors for debugging
        # traceback.print_exc(file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"Unexpected error: {e}", file=sys.stderr)
        traceback.print_exc(file=sys.stderr) # Print full traceback for unexpected errors
        sys.exit(1)

def get_mime_type(file_path):
    file_extension = file_path.split('.')[-1].lower()
    mime_types = {
        'jpg': 'image/jpeg',
        'jpeg': 'image/jpeg',
        'png': 'image/png',
        'gif': 'image/gif',
        'webp': 'image/webp',
        'pdf': 'application/pdf',
        'txt': 'text/plain',
        'text': 'text/plain',
        'csv': 'text/csv',
        'json': 'application/json',
        'html': 'text/html',
        'htm': 'text/html',
        'xml': 'application/xml',
        'audio': 'audio/*', # Generic audio type. More specific types like 'audio/mpeg' can be added
        'mp3': 'audio/mpeg',
        'wav': 'audio/wav',
        'ogg': 'audio/ogg',
        'flac': 'audio/flac',
        'mp4': 'video/mp4',
        'image': 'image/*' # Generic image type
    }
    return mime_types.get(file_extension)

if __name__ == "__main__":
    main()
