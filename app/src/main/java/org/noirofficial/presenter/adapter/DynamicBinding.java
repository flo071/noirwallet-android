package org.noirofficial.presenter.adapter;

public interface DynamicBinding {

    /**
     * Gives the item an opportunity to do work during binding.
     */
    void bind(DataBoundViewHolder holder);

}