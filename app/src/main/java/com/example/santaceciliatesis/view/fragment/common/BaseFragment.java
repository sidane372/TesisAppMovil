package com.example.santaceciliatesis.view.fragment.common;

import android.view.View;
import android.widget.*;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.example.santaceciliatesis.*;
import com.example.santaceciliatesis.data.AppContainer;
import com.example.santaceciliatesis.view.activity.NavigationHost;

public abstract class BaseFragment extends Fragment {
    protected BaseFragment(int layout) {
        super(layout);
    }

    protected AppContainer deps() {
        return ((SantaCeciliaApplication) requireActivity().getApplication()).getContainer();
    }

    protected NavigationHost host() {
        return (NavigationHost) requireActivity();
    }

    protected <T extends View> T v(int id) {
        return requireView().findViewById(id);
    }

    protected void text(int id, CharSequence value) {
        ((TextView) v(id)).setText(value);
    }

    protected void visible(int id, boolean value) {
        v(id).setVisibility(value ? View.VISIBLE : View.GONE);
    }

    protected void enabled(int id, boolean value) {
        v(id).setEnabled(value);
    }

    protected void list(int id, RecyclerView.Adapter<?> adapter) {
        RecyclerView list = v(id);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setNestedScrollingEnabled(false);
        list.setAdapter(adapter);
    }

    protected void spinner(int id, java.util.List<String> labels) {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(requireContext(), R.layout.item_selector, labels);
        adapter.setDropDownViewResource(R.layout.item_selector);
        ((Spinner) v(id)).setAdapter(adapter);
    }
}
