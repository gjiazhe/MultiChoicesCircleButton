package com.gjiazhe.multichoicescirclebutton.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.gjiazhe.multichoicescirclebutton.MultiChoicesCircleButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    MultiChoicesCircleButton multiChoicesCircleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<MultiChoicesCircleButton.Item> buttonItems = new ArrayList<>();
        MultiChoicesCircleButton.Item item1 = new MultiChoicesCircleButton.Item("Like",getResources().getDrawable(R.drawable.icon1), 30);
        buttonItems.add(item1);
        MultiChoicesCircleButton.Item item2 = new MultiChoicesCircleButton.Item("Message", getResources().getDrawable(R.drawable.icon2), 90);
        buttonItems.add(item2);
        MultiChoicesCircleButton.Item item3 = new MultiChoicesCircleButton.Item("Tag", getResources().getDrawable(R.drawable.icon3), 150);
        buttonItems.add(item3);

        multiChoicesCircleButton = (MultiChoicesCircleButton) findViewById(R.id.multiChoicesCircleButton);
        multiChoicesCircleButton.setButtonItems(buttonItems);
    }
}
