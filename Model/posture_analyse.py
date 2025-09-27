import os
import pandas as pd
import json
import numpy as np
import glob

# --- Helper function to calculate angles ---
def calculate_angle(a, b, c):
    """Calculates the angle between three points in 2D space."""
    a, b, c = np.array(a), np.array(b), np.array(c)
    ba, bc = a - b, c - b
    dot_product = np.dot(ba, bc)
    magnitude_ba = np.linalg.norm(ba)
    magnitude_bc = np.linalg.norm(bc)
    cosine_angle = np.clip(dot_product / (magnitude_ba * magnitude_bc + 1e-6), -1.0, 1.0)
    return np.degrees(np.arccos(cosine_angle))

def extract_features_from_file(filepath):
    """Loads a JSON file and extracts the three key posture angles."""
    try:
        with open(filepath, 'r') as f:
            data = json.load(f)
    except Exception:
        return None

    if not data.get('persons'):
        return None

    keypoints = {part['part_name']: part for part in data['persons'][0]['keypoints']}
    
    # Use the side with the highest confidence for ear and shoulder
    side = 'left' if keypoints.get('left_ear', {}).get('confidence', 0) > keypoints.get('right_ear', {}).get('confidence', 0) else 'right'
    
    required_parts = {
        'ear': keypoints.get(f'{side}_ear'),
        'shoulder': keypoints.get(f'{side}_shoulder'),
        'hip': keypoints.get(f'{side}_hip'),
        'knee': keypoints.get(f'{side}_knee'),
        'ankle': keypoints.get(f'{side}_ankle')
    }
    
    if not all(required_parts.values()):
        return None

    coords = {k: [v['x'], v['y']] for k, v in required_parts.items()}

    # Calculate the three main angles as you defined
    neck_angle = calculate_angle(coords['hip'], coords['shoulder'], coords['ear'])
    back_angle = calculate_angle(coords['knee'], coords['hip'], coords['shoulder'])
    legs_angle = calculate_angle(coords['ankle'], coords['knee'], coords['hip'])
    
    return {
        'neck_angle': neck_angle,
        'back_angle': back_angle,
        'legs_angle': legs_angle
    }

def analyze_posture_data(base_dir='webcam_captures/json'):
    """Analyzes all JSON files to find angle ranges for each posture."""
    all_features = []
    
    if not os.path.isdir(base_dir):
        print(f"Error: Base directory '{base_dir}' not found.")
        print("Please make sure your posture folders are inside a folder named 'webcam_captures/json'.")
        return

    for posture_label in os.listdir(base_dir):
        posture_dir = os.path.join(base_dir, posture_label)
        if not os.path.isdir(posture_dir):
            continue
            
        json_files = glob.glob(os.path.join(posture_dir, '*.json'))
        for file_path in json_files:
            features = extract_features_from_file(file_path)
            if features:
                features['posture'] = posture_label
                all_features.append(features)
    
    if not all_features:
        print("Could not extract any features. Check the JSON files and folder structure.")
        return

    df = pd.DataFrame(all_features)
    
    pd.set_option('display.precision', 2)
    summary = df.groupby('posture').agg(['min', 'mean', 'max'])
    
    print("--- Posture Angle Analysis ---")
    print("This table shows the minimum, average, and maximum angles for each posture type in your data.")
    print(summary)
    
    if 'straight' in summary.index:
        straight_stats = summary.loc['straight']
        print("\n--- RECOMMENDED THRESHOLDS FOR A 'GOOD' POSTURE ---")
        print("Based on your 'straight' posture samples, use these values in your corrector script:")
        print(f"Neck Angle ('Hip-Shoulder-Ear'):  Min={straight_stats[('neck_angle', 'min')]:.0f}, Max={straight_stats[('neck_angle', 'max')]:.0f}")
        print(f"Back Angle ('Knee-Hip-Shoulder'):  Min={straight_stats[('back_angle', 'min')]:.0f}, Max={straight_stats[('back_angle', 'max')]:.0f}")
        print(f"Legs Angle ('Ankle-Knee-Hip'):   Min={straight_stats[('legs_angle', 'min')]:.0f}, Max={straight_stats[('legs_angle', 'max')]:.0f}")

if __name__ == "__main__":
    analyze_posture_data(base_dir='webcam_captures/json')
