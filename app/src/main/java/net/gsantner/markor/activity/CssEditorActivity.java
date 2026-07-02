/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import net.gsantner.markor.R;

/**
 * A dedicated full-screen editor activity for editing custom Markdown preview CSS code.
 * Launched from Settings when the user taps the "Custom Preview CSS" preference.
 * Provides a multiline monospace EditText for comfortable CSS editing (Typora-style).
 */
public class CssEditorActivity extends MarkorBaseActivity {

    private EditText _cssEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.css_editor_activity);

        // Setup toolbar with back navigation
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        _cssEditor = findViewById(R.id.css_editor_text);

        // Load current CSS code from preferences
        String currentCss = _appSettings.getCustomMarkdownCssCode();
        Log.d("MarkorCSS", "EDITOR_LOAD cssLen=" + currentCss.length());
        if (!TextUtils.isEmpty(currentCss)) {
            _cssEditor.setText(currentCss);
            _cssEditor.setSelection(currentCss.length());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.css_editor__menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save_css) {
            saveCss();
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveCss() {
        String css = _cssEditor.getText().toString();
        Log.d("MarkorCSS", "EDITOR_SAVE cssLen=" + css.length()
                + " preview=" + (css.length() > 100 ? css.substring(0, 100) : css));
        _appSettings.setString(R.string.pref_key__custom_markdown_css, css);
        Toast.makeText(this, R.string.custom_css_saved, Toast.LENGTH_SHORT).show();
    }
}
