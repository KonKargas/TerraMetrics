# Data sources and redistribution notes

This documentation reflects the four provenance records present in TerraMetrics' SQLite `sources` table. These are **historical imports**, not live API integrations.

| Data family | Recorded provenance | Attribution / reuse consideration |
|---|---|---|
| Temperature | Food and Agriculture Organization of the United Nations (FAO), FAOSTAT Climate Change / Temperature change. Accessed 2023-03-28. | Source metadata states **CC BY-NC-SA 3.0 IGO**. |
| Disasters | Emergency Events Database (**EM-DAT**), CRED / UCLouvain, Brussels. Accessed 2023-03-31. | Check the EM-DAT data-use terms and any applicable redistribution limits before publishing the underlying dataset. |
| Land cover | FAOSTAT Land Cover; IMF staff calculations. Accessed 2022-09-30. | Source metadata states **CC BY-NC-SA 3.0 IGO**; review terms and derived-data obligations. |
| Forest/carbon-related indicators | FAOSTAT Land Use; IMF staff calculations. Accessed 2023-03-28. | Confirm applicable source/derivative licensing and citation requirements. |

Source websites: [FAOSTAT](https://www.fao.org/faostat/en/#home) · [EM-DAT](https://www.emdat.be/)

The exact imported citations can be viewed in the **Data Sources** page or queried from the `sources` table.

## Before publishing

1. Verify whether the included raw CSVs, generated `tf_data` CSVs, and populated `data/climate.db` may be **publicly redistributed** under their respective source terms.
2. If permissions are unclear, omit the data files and DB from the public repository, and document how an authorized user can acquire/build the datasets. The current application needs a populated database to function.
3. Keep any chosen software `LICENSE` separate from data attribution and licensing.
4. Re-check source attribution if datasets are updated.
