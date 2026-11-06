package com.ammar.sharing.custom.ui;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.app.AppCompatViewInflater;
import androidx.appcompat.widget.*;
import com.ammar.sharing.common.utils.AppAppearance;

public class SharingViewInflater extends AppCompatViewInflater {
  @Override
  protected AppCompatTextView createTextView(Context c, AttributeSet a) {
    AppCompatTextView view = super.createTextView(c, a);
    AppAppearance.apply(view);
    return view;
  }

  @Override
  protected AppCompatButton createButton(Context c, AttributeSet a) {
    AppCompatButton view = super.createButton(c, a);
    AppAppearance.apply(view);
    return view;
  }

  @Override
  protected AppCompatEditText createEditText(Context c, AttributeSet a) {
    AppCompatEditText view = super.createEditText(c, a);
    AppAppearance.apply(view);
    return view;
  }

  @Override
  protected AppCompatCheckBox createCheckBox(Context c, AttributeSet a) {
    AppCompatCheckBox view = super.createCheckBox(c, a);
    AppAppearance.apply(view);
    return view;
  }
}
