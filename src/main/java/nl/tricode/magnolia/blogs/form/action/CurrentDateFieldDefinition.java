package nl.tricode.magnolia.blogs.form.action;

import info.magnolia.ui.form.field.definition.DateFieldDefinition;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Definition for getting current date as default value when using date field type.
 */
public class CurrentDateFieldDefinition extends DateFieldDefinition {

    public CurrentDateFieldDefinition(){
        super.setDefaultValue(getCurrentDate());
    }

    private String getCurrentDate(){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate currentDate = LocalDate.now();
        return formatter.format(currentDate);
    }
}
