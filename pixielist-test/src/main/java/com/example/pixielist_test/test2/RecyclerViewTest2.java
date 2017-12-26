package com.example.pixielist_test.test2;

import android.view.View;
import android.widget.Toast;

import kairischarm.pixielist_annotation.RecyclerView;
import kairischarm.pixielist_annotation.BindMethod;
import kairischarm.pixielist_annotation.ViewHolderMethod;


@RecyclerView(viewHolderLayout = "R.layout.test_view_holder",
                dataClass = RecyclerViewTestDataObject.class)
public class RecyclerViewTest2
{
    @BindMethod
    public static void SetData(final RecyclerViewTest2RecyclerView.TestViewHolder inViewHolder, RecyclerViewTestDataObject inData)
    {
        inViewHolder.mBinding.setData(inData);

        inViewHolder.mBinding.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(inViewHolder.mBinding.getRoot().getContext(), inViewHolder.mBinding.getData().GetText(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @ViewHolderMethod
    public static boolean IsChecked(final RecyclerViewTest2RecyclerView.TestViewHolder inViewHolder)
    {
        return inViewHolder.mBinding.testSwitch.isChecked();
    }
}
