/**
 * Highlight zone updates in different colors
 */

Tapestry.ElementEffect.colorhighlight = function(element) 
{
    return new Effect.Highlight(element, { 
        startcolor : '%s',
        restorecolor: $(element).savedBackgroundColor
    }
    );
};
