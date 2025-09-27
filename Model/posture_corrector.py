import os
import time
import glob
import json
import numpy as np

# --- Helper function to calculate angles ---
def calculate_angle(a, b, c):
    """Calculates the angle between three points in 2D space."""
    a, b, c = np.array(a), np.array(b), np.array(c)
    ba = a - b
    bc = c - b
    
    dot_product = np.dot(ba, bc)
    
    magnitude_ba = np.linalg.norm(ba)
    magnitude_bc = np.linalg.norm(bc)

    cosine_angle = np.clip(dot_product / (magnitude_ba * magnitude_bc + 1e-6), -1.0, 1.0)
    angle = np.arccos(cosine_angle)
    return np.degrees(angle)

# --- The Core Logic: Ergonomic Rules and Thresholds ---
# These thresholds are based on the analysis of your "straight" reference files.
# A buffer of around +/- 5-7 degrees is added to create a comfortable "good" range.
POSTURE_RULES = {
    'neck': {
        'angle_points': ['hip', 'shoulder', 'ear'],
        'min_angle': 145,  # Derived from your data's min of 151
        'max_angle': 175,  # Derived from your data's max of 170
        'low_suggestion': 'FIX: Your neck is bent too far forward. Tuck your chin in.',
        'high_suggestion': 'FIX: Avoid tilting your head too far back.'
    },
    'back': {
        'angle_points': ['knee', 'hip', 'shoulder'],
        'min_angle': 90,   # Derived from your data's min of 88
        'max_angle': 100,  # Derived from your data's max of 103
        'low_suggestion': 'FIX: You are slouching. Sit up straight and engage your core.',
        'high_suggestion': 'FIX: You are leaning back too far. Bring your torso upright.'
    },
    'legs': {
        'angle_points': ['ankle', 'knee', 'hip'],
        'min_angle': 82,   # Derived from your data's min of 87
        'max_angle': 109,  # Derived from your data's max of 104
        'low_suggestion': 'FIX: Your knees are too bent. Lower your feet or raise your chair.',
        'high_suggestion': 'FIX: Your legs are too extended. Place your feet flat on the floor.'
    }
}

def analyze_posture(filepath):
    """Loads a JSON file and analyzes the posture based on the defined rules."""
    try:
        with open(filepath, 'r') as f:
            data = json.load(f)
    except Exception as e:
        return [f"Error reading file: {e}"]

    if not data.get('persons'):
        return ["No person detected in file."]

    keypoints = {part['part_name']: part for part in data['persons'][0]['keypoints']}
    
    # Determine the visible side based on ear confidence
    side = 'left' if keypoints.get('left_ear', {}).get('confidence', 0) > keypoints.get('right_ear', {}).get('confidence', 0) else 'right'
    
    required_parts = [f'{side}_ear', f'{side}_shoulder', f'{side}_hip', f'{side}_knee', f'{side}_ankle']
    coords = {}
    for part in required_parts:
        if part not in keypoints:
            return [f"Critical Error: Missing '{part}' keypoint."]
        coords[part.split('_')[1]] = [keypoints[part]['x'], keypoints[part]['y']]

    analysis_results = []
    
    # Iterate through each rule (neck, back, legs)
    for region, rule in POSTURE_RULES.items():
        p1_name, p2_name, p3_name = rule['angle_points']
        p1, p2, p3 = coords[p1_name], coords[p2_name], coords[p3_name]

        # p2 is the vertex; ensure it's passed as the middle argument
        angle = calculate_angle(p1, p2, p3)

        status = "GOOD"
        suggestion = "No correction needed."
        
        if angle < rule['min_angle']:
            status = "INCORRECT (Too Bent/Forward)"
            suggestion = rule['low_suggestion']
        elif angle > rule['max_angle']:
            status = "INCORRECT (Too Reclined/Extended)"
            suggestion = rule['high_suggestion']
        
        analysis_results.append({
            'region': region.capitalize(),
            'angle': f"{angle:.1f}°",
            'status': status,
            'suggestion': suggestion
        })
        
    return analysis_results

# --- Main Application: Folder Watcher ---
def run_folder_watcher(watch_folder='keypoints_to_analyze'):
    """Continuously watches a folder for new .json files and analyzes them."""
    if not os.path.exists(watch_folder):
        os.makedirs(watch_folder)

    processed_files = set()
    print(f"Starting Posture Corrector. Watching folder: '{watch_folder}'...")
    print("Drop new keypoint JSON files into the folder for analysis. Press CTRL+C to stop.")

    try:
        while True:
            json_files = glob.glob(os.path.join(watch_folder, '*.json'))
            new_files = sorted(list(set(json_files) - processed_files))

            if new_files:
                for file_path in new_files:
                    print(f"\n--- Analyzing File: {os.path.basename(file_path)} ---")
                    results = analyze_posture(file_path)
                    
                    if isinstance(results, list) and isinstance(results[0], dict):
                        print(f"{'Region':<10} | {'Angle':<10} | {'Status':<35} | {'Suggestion'}")
                        print("-" * 110)
                        for res in results:
                            print(f"{res['region']:<10} | {res['angle']:<10} | {res['status']:<35} | {res['suggestion']}")
                    else:
                        print(results[0])

                    processed_files.add(file_path)
            
            time.sleep(2)
    except KeyboardInterrupt:
        print("\nStopping the posture corrector.")

if __name__ == "__main__":
    run_folder_watcher()

