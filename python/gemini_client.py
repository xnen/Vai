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
    prompt = sys.argv[2] if len(sys.argv) > 2 else ""
    file_paths = sys.argv[3:] if len(sys.argv) > 3 else []

    client = genai.Client(api_key=api_key)

    contents = []
    contents.append(types.Part.from_text(prompt))

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

    try:
        response = client.models.generate_content(
            model=model_name,
            contents=contents,
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
        'image': 'image/*' # Generic image type
    }
    return mime_types.get(file_extension)

if __name__ == "__main__":
    main()
