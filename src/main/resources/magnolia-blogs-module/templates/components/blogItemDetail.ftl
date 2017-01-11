[#if (ctx.parameters.name?has_content)]
    [#assign blog = cmsfn.contentByPath("/"+ctx.parameters.name, "collaboration") /]

    <article class="blog-item">
        <h2>${blog.title!"No title found"}</h2>

        [#assign categories = model.getBlogCategories(blog) /]
        [#if (categories)?size > 0 ]
            [#assign blogSummariesLink = cmsfn.link(cmsfn.contentById(content.blogSummariesLink)) /]

            <div class="postdetails">
                <ul class="list-unstyled inline-list blog-categories">
                    [#list categories as category]
                        <li>
                            <a href="${blogSummariesLink}?category=${category.@path}">
                                <i class="fa fa-star">${category.@name}</i>
                            </a>
                        </li>
                    [/#list]
                </ul>
            </div>
        [/#if]

        <section>
            ${blog.message!""}
        </section>

        [#assign author = cmsfn.contentById(blog.author, "contacts" ) /]
        <div class="postdetails">
            <ul class="list-unstyled inline-list blog-info">
                <li><i class="fa fa-calendar">${cmsfn.metaData(blog,"mgnl:created")?date("yyyy-MM-dd")}</i></li>
                <li><i class="fa fa-pencil">${author.firstName} ${author.lastName}</i></li>
            </ul>
        </div>
    </article>
[#else]
    [#if (cmsfn.isEditMode() && !cmsfn.isPreviewMode() && !ctx.parameters.name?has_content)]
        <div class="alert-base alert edit-mode">
            This is a placeholder for the blog detail rendering component which will be shown only in edit/preview mode!
            <a href="" class="close">&times;</a>
        </div>
    [/#if]
[/#if]