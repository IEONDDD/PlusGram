package com.plusgram.app.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.ui.Cells.TextCell;

import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;

public class PlusgramDevelopersActivity extends BaseNekoSettingsActivity {
    private static final String[] DEVELOPERS = {
            "GPT 6 Astra",
            "GPT 6.1 Sol",
            "GPT 6.2 Sol",
            "Kimi 2.8",
            "Kimi 3.1 Pro",
            "Deepseek V4 Pro",
            "Gemini 3.8 Flash"
    };

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.PlusgramDevelopers);
    }

    @Override
    protected void updateRows() {
        super.updateRows();
        for (String developer : DEVELOPERS) {
            addRow();
        }
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new BaseListAdapter(context) {
            @Override
            public int getItemViewType(int position) {
                return TYPE_TEXT;
            }

            @Override
            public boolean isEnabled(RecyclerView.ViewHolder holder) {
                return false;
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, boolean partial) {
                TextCell cell = (TextCell) holder.itemView;
                cell.setText(DEVELOPERS[position], position < DEVELOPERS.length - 1);
            }
        };
    }
}
