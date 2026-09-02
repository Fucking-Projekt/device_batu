/*
 * Copyright (C) 2025 KamiKaonashi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.resolution;

import android.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.lineageos.settings.R;

public class SystemResolutionSettingsFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private ResolutionUtils mResolutionUtils;
    private Spinner mModeSpinner;
    private TextView mSummaryView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.system_resolution_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mResolutionUtils = new ResolutionUtils(getActivity());

        mSummaryView = view.findViewById(R.id.system_resolution_summary);
        mModeSpinner = view.findViewById(R.id.system_resolution_mode);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getActivity(),
                R.array.system_resolution_entries,
                android.R.layout.simple_spinner_dropdown_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mModeSpinner.setAdapter(adapter);
        mModeSpinner.setOnItemSelectedListener(this);

        int state = mResolutionUtils.getGlobalState();
        mModeSpinner.setSelection(stateToPosition(state), false);
        updateSummary(state);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(getResources().getString(R.string.system_resolution_title));
        int actual = mResolutionUtils.getCurrentActualState();
        mModeSpinner.setSelection(stateToPosition(actual), false);
        updateSummary(actual);
    }

    private void updateSummary(int state) {
        String res = mResolutionUtils.getResolutionString(state);
        mSummaryView.setText(getString(R.string.system_resolution_summary, res));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int state = ResolutionUtils.SYSTEM_STATE_MAP[position];
        int current = mResolutionUtils.getGlobalState();
        if (current != state) {
            mResolutionUtils.setGlobalState(state);
            updateSummary(state);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { /* no-op */ }

    private int stateToPosition(int state) {
        int[] map = ResolutionUtils.SYSTEM_STATE_MAP;
        for (int i = 0; i < map.length; i++) {
            if (map[i] == state) return i;
        }
        return 0;
    }
}
