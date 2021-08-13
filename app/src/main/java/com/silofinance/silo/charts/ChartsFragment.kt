package com.silofinance.silo.charts

//import android.os.Bundle
//import android.view.*
//import android.widget.Toast
//import androidx.databinding.DataBindingUtil
//import androidx.fragment.app.Fragment
//import com.silofinance.silo.R
//import com.silofinance.silo.databinding.FragmentChartsBinding
//
//
//class ChartsFragment : Fragment() {
//
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        val binding: FragmentChartsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_charts, container, false)
//
//        setHasOptionsMenu(true)  // Essentially causes a call to onCreateOptionsMenu, which creates the options menu in the toolbar
//
//        return binding.root
//    }
//
//    /** Inflate the (toolbar) menu */
//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        inflater.inflate(R.menu.menu_charts, menu)
//        super.onCreateOptionsMenu(menu, inflater)
//    }
//
//    /** Handle menu item selection */
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.menu_charts_filter -> {
//                Toast.makeText(context, "Filter", Toast.LENGTH_SHORT).show()  //todo extract
//                true
//            }
//            R.id.menu_charts_timescale -> {
//                Toast.makeText(context, "Timescale", Toast.LENGTH_SHORT).show()  //todo extract
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
//}
//
////Spending, cash flow, net worth