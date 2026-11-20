package com.plusgram.app.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
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
    private static final int[] REPLIES = {
            R.array.PlusgramCouncilAstra, R.array.PlusgramCouncilSol61, R.array.PlusgramCouncilSol62,
            R.array.PlusgramCouncilKimi28, R.array.PlusgramCouncilKimi31,
            R.array.PlusgramCouncilDeepseek, R.array.PlusgramCouncilGemini
    };
    private final int[] nextReply = new int[DEVELOPERS.length];

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.PlusgramDeveloperCouncil);
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
        if (getParentActivity() == null) return;
        String[] replies = getParentActivity().getResources().getStringArray(REPLIES[position]);
        int reply = nextReply[position];
        nextReply[position] = (reply + 1) % replies.length;
        showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                .setTitle(DEVELOPERS[position]).setMessage(replies[reply])
                .setPositiveButton(getString(R.string.OK), null).create());
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new BaseListAdapter(context) {
            @Override
            public int getItemViewType(int position) {
                return TYPE_TEXT;
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, boolean partial) {
                TextCell cell = (TextCell) holder.itemView;
                cell.setText(DEVELOPERS[position], position < DEVELOPERS.length - 1);
            }
        };
    }
}
